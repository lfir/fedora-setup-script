## Fedora setup script

Ansible playbook that installs most KDE applications and many development tools on
Fedora 42 systems, using the [KDE Edition](https://fedoraproject.org/kde) as a starting point.
It also sets some configuration options useful for development work and system administration.

#### Notes

- Free useful apps available in a portable format (i.e. AppImage)

  - [7zip console version](https://sourceforge.net/projects/sevenzip)
  - [AnotherRedisDesktopManager](https://github.com/qishibo/AnotherRedisDesktopManager/releases)
  - [Apache Maven](https://maven.apache.org/download.cgi)
  - [DBeaver](https://github.com/dbeaver/dbeaver/releases)
  - [Eclipse Memory Analyzer Tool stand-alone](https://www.eclipse.org/mat/downloads.php)
  - [Firefox](https://download.mozilla.org/?product=firefox-latest-ssl&os=linux64&lang=en-US)
  - [IntelliJ IDEA community](https://www.jetbrains.com/idea/download/download-thanks.html?platform=linux&code=IIC)
  - [NoSQLBooster4Mongo](https://nosqlbooster.com/downloads)
  - [Postman](https://dl.pstmn.io/download/latest/linux64)
  - [PyCharm community](https://www.jetbrains.com/pycharm/download/download-thanks.html?platform=linux&code=PCC)
  - [Spring Boot CLI](https://docs.spring.io/spring-boot/docs/current/reference/html/getting-started.html#getting-started.installing.cli)
  - [Tor Browser](https://www.torproject.org/download/)
  - [Ventoy](https://github.com/ventoy/Ventoy/releases)
  - [VSCodium](https://github.com/VSCodium/vscodium/releases)

- Repositories that use $releasever need to be checked before performing a
  system upgrade to see if packages for target release are available.

- Run commands

  - Prerequisites

    sudo dnf install ansible

  - Default

    ansible-playbook -K F42_post_install.yml

  - Non-interactive

    ansible-playbook F42_post_install.yml -e "ansible_become_pass=pwd"

    Might fail with empty sudo password like in the live session. A password can be set for the liveuser with command:

    sudo passwd liveuser

    Or disable sudo password for liveuser with sudo visudo and append:

    liveuser ALL=(ALL) NOPASSWD:ALL

  - GUI (partial)

    ./package-selector_gui.sh

    Allows easier selection of packages to install and remove.
