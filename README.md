# smpchatutils

paper(folia)-first chat utilities for low-effort smps

## feature roadmap

- [x] name colours/styles
- [x] permission support (ongoing)
- [x] ignore system
- [x] yaml and sqlite persistence, configurable
- [ ] improved direct messaging
    - [ ] better whisper
    - [ ] reply command
- [ ] line-prefix chat (>, |. ', etc)
- [ ] offline mail queue

## anti-features

- no roles or prefixes/suffices
- no spam filtering
- does not assume 1.19+ chat signage

## permissions

- `smpchatutils.chat.namecolor`
- `smpchatutils.chat.format`
- `smpchatutils.chat.ignore`
- `smpchatutils.reload`

all features can be revoked/granted via the permissions plugin of your choice. however, some sensible defaults are described in the config.

## build

```bash
./gradlew build
```

JAR: `build/libs/smpchatutils-<version>.jar` (`version` in `gradle.properties`; current **0.2.0**).

## requirements

- Java 21
- Paper 1.21+

## license

MIT, see [`LICENSE.txt`](LICENSE.txt).
