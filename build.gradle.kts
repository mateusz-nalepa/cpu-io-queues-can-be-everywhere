plugins {
	kotlin("jvm") version "2.2.21"
}

group = "com.nalepa"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(24)
	}
}

repositories {
	mavenCentral()
}
