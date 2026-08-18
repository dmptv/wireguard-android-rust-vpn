pipeline {
    agent any

    environment {
        // Тот же NDK, которым мы кросс-компилировали Rust-ядро вручную.
        ANDROID_NDK_HOME = "${HOME}/Library/Android/sdk/ndk/27.0.12077973"
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

        stage('Archive APK') {
            steps {
                archiveArtifacts artifacts: 'app/build/outputs/apk/debug/*.apk', fingerprint: true
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
