## Fedora setup script

Java (21+) program that installs most KDE applications and many development tools on
Fedora 42 systems, using the [KDE Edition](https://fedoraproject.org/kde),
which comes with Java 21 preinstalled, as a starting point.
It also sets some configuration options useful for development work and system administration.

Current actions that can be applied (each needs to be confirmed first):

1. Install RPMFusion repositories. 
Adds free and nonfree RPMFusion repos and imports their GPG keys.

2. Install additional packages with DNF.
Installs extra packages listed in dnf-install.cf, with the
option to skip selected packages interactively.

3. Remove unnecessary packages.
Removes unwanted packages from dnf-remove.cf and performs autoremove.

4. Install Flatpak applications.
Adds the Flathub remote and installs Flatpaks from flatpak-install.cf.

5. Ensure groups exist and add user.
Creates and assigns admin-related groups such as _docker_, _libvirt_, and _vboxusers_.

6. Enable and start Cockpit.
Enables and starts the systemd service of the Cockpit web-based management interface.

### Run commands

- App

  ```
  java src/main/java/cf/maybelambda/fedora/PostInstallUpdater.java
  ```

`--help` can be passed to see available options.

- Tests

  ```
  ./mvnw test
  ```

Both should be used as a non-root user. The app invokes _sudo_ as needed.

#### Notes

- Free useful apps available in a portable format (i.e. AppImage)

  - [7zip console version](https://sourceforge.net/projects/sevenzip)
  - [AnotherRedisDesktopManager](https://github.com/qishibo/AnotherRedisDesktopManager/releases)
  - [Apache Maven](https://maven.apache.org/download.cgi)
  - [DBeaver](https://github.com/dbeaver/dbeaver/releases)
  - [DbGate](https://github.com/dbgate/dbgate/releases)
  - [Eclipse Memory Analyzer Tool stand-alone](https://www.eclipse.org/mat/downloads.php)
  - [Firefox](https://download.mozilla.org/?product=firefox-latest-ssl&os=linux64&lang=en-US)
  - [IntelliJ IDEA community](https://www.jetbrains.com/idea/download/download-thanks.html?platform=linux&code=IIC)
  - [NoSQLBooster4Mongo](https://nosqlbooster.com/downloads)
  - [Postman](https://dl.pstmn.io/download/latest/linux64)
  - [PyCharm community](https://www.jetbrains.com/pycharm/download/download-thanks.html?platform=linux&code=PCC)
  - [SourceGit](https://github.com/sourcegit-scm/sourcegit/releases)
  - [Spring Boot CLI](https://docs.spring.io/spring-boot/docs/current/reference/html/getting-started.html#getting-started.installing.cli)
  - [Tor Browser](https://www.torproject.org/download/)
  - [Ventoy](https://github.com/ventoy/Ventoy/releases)
  - [VSCodium](https://github.com/VSCodium/vscodium/releases)

- Repositories that use $releasever need to be checked before performing a
  system upgrade to see if packages for target release are available.

