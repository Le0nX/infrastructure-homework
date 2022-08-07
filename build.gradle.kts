import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.*

plugins {
	id("org.springframework.boot") version "2.2.1.BUILD-SNAPSHOT"
	id("io.spring.dependency-management") version "1.0.8.RELEASE"
	kotlin("jvm") version "1.3.50"
	kotlin("plugin.spring") version "1.3.50"
	id("org.jetbrains.kotlin.plugin.jpa") version "1.3.50" apply false
}

allprojects {
	group = "com.stringconcat"
	version = "0.0.1-SNAPSHOT"

	repositories {
		mavenCentral()
		jcenter()
		maven { url = uri("https://repo.spring.io/milestone") }
		maven { url = uri("https://repo.spring.io/snapshot") }
	}

	tasks.withType<KotlinCompile> {
		kotlinOptions {
			freeCompilerArgs = listOf("-Xjsr305=strict")
			jvmTarget = "1.8"
		}
	}

}

java.sourceCompatibility = JavaVersion.VERSION_1_8

val developmentOnly by configurations.creating
configurations {
	runtimeClasspath {
		extendsFrom(developmentOnly)
	}
}



dependencies {
	// spring modules
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-rest")

	// kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	implementation(project(":presentation"))
	implementation(project(":persistence"))
	implementation(project(":useCasePeople"))
	implementation(project(":businessPeople"))
	implementation(project(":quoteGarden"))
	implementation(project(":avatarsDicebear"))

	// dev tools
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	//persistance
	implementation("org.postgresql:postgresql:42.3.4")
	implementation("org.liquibase:liquibase-core:4.9.1")

	// tests
	testCompile("org.junit.jupiter:junit-jupiter-api:5.8.2")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("io.projectreactor:reactor-test")
}

tasks.test {
	useJUnitPlatform()
}

val gitCommitHash: String by lazy {
	if (project.hasProperty("buildSHA")) {
		project.property("buildSHA").toString()
	} else "no git info"
}

val currentTime: String by lazy {
	SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SZ").run {
		timeZone = TimeZone.getTimeZone("UTC")
		format(Date())
	}
}

tasks {

	register<Copy>("installLocalGitHook") {
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(layout.projectDirectory.file("tools/git-hook/pre-push"))
		into(layout.projectDirectory.dir(".git/hooks"))
		fileMode = Integer.parseUnsignedInt("755", 8)
	}

	register("setup_banner") {
		doFirst {
			file("src/main/resources/le0nx-banner.txt").writeText(
				"""
					
				/**************************************************/
					BUILD SHA: $gitCommitHash
					VERSION: $version
					TIME: $currentTime
				/**************************************************/
				
				""".trimIndent()
			)
		}
	}

	bootRun {
		dependsOn(":installLocalGitHook")
		dependsOn(":setup_banner")
		mustRunAfter(":setup_banner")
		if (project.hasProperty("args")) {
			args(project.property("args").toString().split(","))
		}
	}

	bootJar {
		dependsOn(":installLocalGitHook")
		dependsOn(":setup_banner")
		mustRunAfter(":setup_banner")
	}

}