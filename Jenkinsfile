pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        // local.properties (which normally holds sdk.dir) is intentionally
        // gitignored — it's machine-specific — so Jenkins clones the repo
        // into a fresh workspace with no local.properties at all. Gradle's
        // Android plugin also accepts the SDK location via ANDROID_HOME.
        ANDROID_HOME = "${HOME}/Library/Android/sdk"
        ANDROID_NDK_HOME = "${HOME}/Library/Android/sdk/ndk/27.0.12077973"
        // Jenkins runs as a background service and does not source shell
        // profile files, so ~/.cargo/bin (where rustup installs cargo and
        // cargo-ndk) is missing from PATH by default.
        PATH = "${HOME}/.cargo/bin:${env.PATH}"
    }

    stages {
        stage('Rust: test') {
            steps {
                dir('rust/vpn-core') {
                    sh 'cargo test'
                }
            }
        }

        stage('Rust: cross-compile for Android') {
            steps {
                dir('rust/vpn-core') {
                    sh 'cargo ndk -t arm64-v8a -t x86_64 -o ../../jniLibs build'
                }
            }
        }

        stage('Android: build') {
            steps {
                sh './gradlew :app:assembleDebug --console=plain'
            }
        }

        stage('Publish to Nexus') {
            // Feature branches only need to prove the build succeeds;
            // publishing an artifact happens only from main.
            when {
                branch 'main'
            }
            steps {
                echo 'Publish stage placeholder — uploads the APK to the Nexus repository.'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'app/build/outputs/apk/debug/*.apk', allowEmptyArchive: true, fingerprint: true
        }
    }
}
