# Backup checksum regression tests

The same 15-test contract runs in two environments:

- Android unit tests: `sh gradlew testDebugUnitTest --tests com.example.BackupRepositoryChecksumTest`, using Robolectric API 28 and Android's `org.json`.
- Standalone JVM: `python3 tools/test_backup_jvm.py`, using the actual production Kotlin models, DAOs, repositories and backup code, with an Android-derived JSON library and read-only fake DAOs.

The standalone path requires Python 3.8+ and Java 11+, downloads approximately 61 MiB once, pins every artifact by version and SHA-256, and stores dependencies under `.verification-cache/backup-jvm`. Use `--offline` after the cache has been populated. It never reads phone files, API keys or real user data. Its synthetic fixtures include Bengali, emoji, quotes, backslashes, clips and linked notes.

Coverage includes unchanged export/import preview, changed clip/note/tag content, removed clips, legacy backups without a checksum, malformed checksums, checksum case, JSON whitespace, malformed JSON, unsupported schema, empty datasets, missing arrays and wrong payload types. Preview tests fail on any attempted DAO write and require invalid backups to fail before database snapshots are read.

This verifies accidental-corruption detection. The checksum is the existing first eight bytes of SHA-256, serialized as 16 hexadecimal characters. It is not an authenticity guarantee; a person who edits a backup can recompute or remove the checksum. Legacy backups without a checksum intentionally retain their previous import behavior.

The standalone path does not compile the entire Android app or verify Room queries, generated code, database transactions, UI, services, WorkManager, installation or a device restore. Those checks remain separate prerequisites before merging the backup fix.
