#!/usr/bin/env python3
"""Run the backup contract against real Kotlin sources without an Android SDK.

Downloads version- and SHA-256-pinned public dependencies on first use (about
61 MiB). Fake DAOs permit reads only. This does not run Room SQL, Gradle, UI,
Android services, or device storage integration.
"""

import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import urllib.request


def main():
    repo = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cache-dir", type=Path, default=repo / ".verification-cache" / "backup-jvm")
    parser.add_argument("--production-root", type=Path, default=repo,
                        help="Alternate checkout whose production code should be tested")
    parser.add_argument("--offline", action="store_true", help="Fail if a pinned dependency is not cached")
    args = parser.parse_args()
    java = shutil.which("java")
    if java is None:
        parser.error("Java 11 or newer is required on PATH")
    cache = args.cache_dir.resolve()
    cache.mkdir(parents=True, exist_ok=True)
    dependencies = json.loads((repo / "tools" / "backup-jvm-deps.json").read_text())

    def prepare(entry):
        target = cache / entry["name"]
        if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest() == entry["sha256"]:
            return target
        if args.offline:
            raise RuntimeError(f"Missing or invalid cached dependency: {target.name}")
        with urllib.request.urlopen(entry["url"], timeout=60) as response:
            data = response.read()
        if hashlib.sha256(data).hexdigest() != entry["sha256"]:
            raise RuntimeError(f"Dependency SHA-256 mismatch: {target.name}")
        with tempfile.NamedTemporaryFile(dir=cache, delete=False) as output:
            staged = Path(output.name)
            try:
                output.write(data)
                output.close()
                staged.replace(target)
            finally:
                staged.unlink(missing_ok=True)
        return target

    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        jars = list(pool.map(prepare, dependencies))
    classpath = os.pathsep.join(str(path) for path in jars)
    production = args.production_root.resolve() / "app/src/main/java/com/example"
    sources = [production / relative for relative in (
        "domain/model/Models.kt", "data/local/Entities.kt", "data/local/Daos.kt",
        "data/repository/ClipRepository.kt", "data/repository/NoteRepository.kt",
        "data/repository/BackupRepository.kt",
    )]
    sources.extend((repo / "app/src/test/java/com/example/BackupRepositoryChecksumContract.kt",
                    repo / "tools/jvm/BackupRepositoryChecksumJvmTest.kt"))
    for source in sources:
        if not source.is_file():
            raise RuntimeError(f"Missing source: {source}")
    with tempfile.TemporaryDirectory(prefix="classes-", dir=cache) as classes:
        subprocess.run([
            java, "-Xmx768m", "-cp", classpath, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
            "-no-stdlib", "-no-reflect", "-jvm-target", "11", "-classpath", classpath,
            "-d", classes, *(str(source) for source in sources),
        ], check=True, timeout=120)
        subprocess.run([
            java, "-cp", classes + os.pathsep + classpath, "org.junit.runner.JUnitCore",
            "com.example.BackupRepositoryChecksumJvmTest",
        ], check=True, timeout=60)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, RuntimeError, ValueError, subprocess.SubprocessError) as error:
        print(f"Backup verification failed: {error}", file=sys.stderr)
        sys.exit(1)
