pipeline {
    // Один встроенный узел на этой машине — как и в gRPC_Wallet_Demo, agent any
    // достаточно, отдельных agent-нод с лейблами тут не настроено.
    agent any

    options {
        // Fail fast, если сборка зависнет — та же защита, что в gRPC_Wallet_Demo.
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        ANDROID_NDK_HOME = "${HOME}/Library/Android/sdk/ndk/27.0.12077973"
        // Jenkins — фоновый сервис (launchd), не подхватывает ~/.zprofile/~/.zshrc,
        // поэтому cargo/cargo-ndk (стоят через rustup в ~/.cargo/bin) не видны без этого.
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
            // Как и Release-стадия в gRPC_Wallet_Demo: PR/feature-ветки только
            // доказывают, что всё собирается; публикация артефакта — только с main.
            when {
                branch 'main'
            }
            steps {
                echo 'Publish stage — здесь будет загрузка APK в Nexus (raw-репозиторий)'
                // Реальная команда появится, когда настроим Nexus-репозиторий и credentials:
                // sh 'curl -u $NEXUS_USER:$NEXUS_PASS --upload-file app/build/outputs/apk/debug/app-debug.apk http://localhost:8081/repository/vpnclient-apks/app-debug.apk'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'app/build/outputs/apk/debug/*.apk', allowEmptyArchive: true, fingerprint: true
        }
    }
}
