# smpchatutils

paper(folia)-first plugin for general-purpose chat utilities for low-effort smps.

## feature roadmap:
- [x] name colours/styles
- [x] permission support (ongoing)
- [ ] ignore users
- [ ] improved dms
    - [ ] better whisper
    - [ ] reply command
- [ ] line-prefix chat (>, |, ', etc.)
- [ ] offline mail queue
- [x] name-color persistence: `storage.type` **yaml** or **sqlite** (`storage.sqlite-file`)

## anti-features:
- no roles or prefixes/suffices
- no spam filtering
- does not assume 1.19+ chat signage

## permissions

all features should be able to be revoked with a permissions plugin of your choice. some sensible defaults have been described in the config.yml however.

## build

```bash
./gradlew build
```

JAR: `build/libs/smpchatutils-<version>.jar` (`version` in `gradle.properties`; current **0.0.2**).

## requirements

- java 21  
- paper 1.21+ (see `build.gradle.kts`)
