#!/bin/bash
# missing pkgs pcsx2 playonlinux nitroshare preload bleachbit

# Enable docker ce repository
sudo dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo

# Enable insync repository
sudo rpm --import https://d2t3ff60b2tol4.cloudfront.net/repomd.xml.key

ins=$'[insync]
name=insync repo
baseurl=http://yum.insync.io/fedora/$releasever/
gpgcheck=1
gpgkey=https://d2t3ff60b2tol4.cloudfront.net/repomd.xml.key
enabled=1
metadata_expire=120m'

echo "$ins" | sudo tee '/etc/yum.repos.d/insync.repo'

# Enable rpmfusion repositories
sudo sh -c "dnf install https://download1.rpmfusion.org/free/fedora/\
rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm \
https://download1.rpmfusion.org/nonfree/fedora/\
rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm"

# Enable vscode repository
sudo rpm --import https://packages.microsoft.com/keys/microsoft.asc

vsc=$'[code]
name=Visual Studio Code
baseurl=https://packages.microsoft.com/yumrepos/vscode
enabled=1
gpgcheck=1
gpgkey=https://packages.microsoft.com/keys/microsoft.asc'

echo "$vsc" | sudo tee '/etc/yum.repos.d/vscode.repo'

# Update base system
sudo dnf --refresh upgrade

# Install applications
devel=(code git java-latest-openjdk-devel java-latest-openjdk-javadoc meld \
       nodejs-yarn npm pipenv python3-devel python3-ipython python3-virtualenv \
       ShellCheck umbrello)
ed=(hunspell-en hunspell-es libreoffice-core pspp)
games=(desmume dolphin-emu knights pcsxr q4wine steam \
       visualboyadvance-m wine winetricks)
graph=(gwenview kcolorchooser kolourpaint okular xchm)
hw=(radeontop xorg-x11-drv-amdgpu)
inet=(filezilla firefox insync kget konversation remmina transmission-qt wireshark)
kdebase=(bluedevil breeze-icon-theme cagibi colord-kde dolphin \
         firewall-config ffmpegthumbnailer glibc-all-langpacks gnome-keyring-pam kcm_systemd \
         kcron kde-gtk-config kde-print-manager kde-settings-pulseaudio kde-style-breeze \
         kdegraphics-thumbnailers kdeplasma-addons kdialog kdnssd kf5-akonadi-server \
         kf5-akonadi-server-mysql kf5-baloo-file kf5-kipi-plugins kfind khotkeys \
         kinfocenter kmenuedit konsole5 kscreen kscreenlocker ksshaskpass ksysguard \
         kwalletmanager5 kwebkitpart kwin NetworkManager-config-connectivity-fedora \
         pam-kwallet phonon-qt5-backend-gstreamer pinentry-qt plasma-breeze plasma-desktop \
         plasma-desktop-doc plasma-drkonqi plasma-nm plasma-nm-l2tp plasma-nm-openconnect \
         plasma-nm-openswan plasma-nm-openvpn plasma-nm-pptp plasma-nm-vpnc plasma-pa \
         plasma-user-manager plasma-vault plasma-workspace plasma-workspace-geolocation \
         polkit-kde qt5-qtbase-gui qt5-qtdeclarative sddm sddm-breeze sddm-kcm \
         setroubleshoot sni-qt system-config-keyboard system-config-language)
misc=(akmods akmod-VirtualBox dkms flatpak fuse-encfs grub-customizer hddtemp iftop iotop \
      kernel-devel libvirt-bash-completion lm_sensors lshw mate-themes \
      moreutils moreutils-parallel ncdu p7zip p7zip-plugins papirus-icon-theme \
      policycoreutils-gui procmail qemu-kvm ranger SDL2-devel smartmontools stress \
      sysstat telnet tldr unrar wget whois xdotool yum-utils)
multimedia=(asciinema bchunk vlc)
system=(beesu cockpit cockpit-machines cockpit-selinux containerd.io docker-ce docker-ce-cli \
        docker-compose finger gnome-nettool gparted grsync htop kde-partitionmanager \
        ksystemlog tmux virt-manager VirtualBox VirtualBox-server)
utils=(ark filelight gtkhash kate kcalc kcharselect keepassxc kgpg knotes krename \
       nfoview spectacle)

sudo dnf install "${devel[@]}" "${ed[@]}" "${games[@]}" "${graph[@]}" "${hw[@]}" \
                 "${inet[@]}" "${kdebase[@]}" "${misc[@]}" "${multimedia[@]}" \
                 "${system[@]}" "${utils[@]}"

# Enable flathub repository
flatpak remote-add --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo

# Create applications dir and switch to it
mkdir "$HOME/Applications" && cd "$_" || return

# Install chrome, dbvisualizer, multibootusb rpms
latestDbvis="$(wget -O - https://www.dbvis.com/download | grep -m 1 -o 'https://.*rpm')"
latestMbusb="https://github.com$(wget -O - \
https://github.com/mbusb/multibootusb/releases/latest | grep -Po '/.+[0-9]\.noarch\.rpm')"
wget https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm
wget "$latestDbvis"
wget -O mbusb.rpm "$latestMbusb"

sudo dnf install ./*.rpm

# Remove downloaded rpms
rm -f ./*.rpm

# Add current user to groups
sudo groupmems -a "$USER" -g docker -g libvirt -g vboxsf -g vboxusers

# Enable cockpit autostart
sudo systemctl enable --now cockpit.socket

# Cleanup
sudo dnf autoremove
sudo dnf clean packages
