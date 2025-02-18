## Aegis (*tarcza*)

Distributed messaging framework designed for packet handling, async communication, pub-sub,
key-value storage, distributed locking.

### Get started

##### Add repository

```kotlin
maven("https://repo.rubymc.pl/releases")
```

##### Add dependencies

###### Core
```kotlin
implementation("io.mikeamiry.aegis:aegis:2.0.10")
```

###### Codec (You can create your own, implement io.mikeamiry.aegis.codec.Codec)
```kotlin
implementation("io.mikeamiry.aegis:aegis-codec-fury:2.0.10")
implementation("io.mikeamiry.aegis:aegis-codec-jackson:2.0.10")
```
