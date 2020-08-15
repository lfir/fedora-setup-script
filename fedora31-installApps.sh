#!/bin/bash

# Enable Docker CE repository
sudo dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo

# Enable Insync repository
sudo rpm --import https://d2t3ff60b2tol4.cloudfront.net/repomd.xml.key

ins=$'[insync]
name=insync repo
baseurl=http://yum.insync.io/fedora/$releasever/
gpgcheck=1
gpgkey=https://d2t3ff60b2tol4.cloudfront.net/repomd.xml.key
enabled=1
metadata_expire=120m'

echo "$ins" | sudo tee '/etc/yum.repos.d/insync.repo'

# Enable RPMFusion repositories
sudo sh -c "dnf install https://download1.rpmfusion.org/free/fedora/\
rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm \
https://download1.rpmfusion.org/nonfree/fedora/\
rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm"

# Enable VSCode repository
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

# Install package groups excluding unneeded apps
sudo dnf -x akregator -x dnfdragora -x kaddressbook -x kamera -x kmag \
  -x kmail -x kmousetool -x kmouth -x konqueror -x kontact -x korganizer -x kruler -x kwrite \
  -x plasma-discover -x plasma-pk-updates groupinstall Fonts kde-desktop LibreOffice

# Install additional applications
devel=(code git java-latest-openjdk-devel java-latest-openjdk-javadoc meld nodejs-yarn npm pipenv \
  python3-devel python3-ipython python3-virtualenv ShellCheck umbrello)
ed=(hunspell-en hunspell-es pspp)
games=(desmume knights pcsx2 pcsxr q4wine steam visualboyadvance-m wine winetricks)
graph=(kolourpaint okular xchm)
hw=(radeontop xorg-x11-drv-amdgpu)
inet=(filezilla firefox insync konversation remmina transmission-qt wireshark)
misc=(akmod-VirtualBox dkms dnf-utils flatpak fuse-encfs grub-customizer hddtemp iftop iotop \
  kernel-devel libvirt-bash-completion lm_sensors lshw mate-themes moreutils-parallel ncdu p7zip \
  p7zip-plugins papirus-icon-theme policycoreutils-gui python-django-bash-completion \
  python3-click-completion qemu-kvm ranger smartmontools stress sysstat telnet tldr unrar wget \
  xdotool)
multimedia=(asciinema bchunk vlc)
system=(beesu cockpit cockpit-docker cockpit-machines cockpit-selinux \
  containerd.io docker-ce docker-compose finger gnome-nettool gparted grsync htop \
  ksystemlog tmux virt-manager VirtualBox VirtualBox-server)
utils=(ark filelight gtkhash kate keepassxc knotes krename nfoview)

sudo dnf install "${devel[@]}" "${ed[@]}" "${games[@]}" "${graph[@]}" "${hw[@]}" \
  "${inet[@]}" "${misc[@]}" "${multimedia[@]}" "${system[@]}" "${utils[@]}"

# Enable Flathub repository
flatpak remote-add --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo

# Create applications dir and switch to it
mkdir "$HOME/Applications" && cd "$_" || return

# Install Chrome, DBVisualizer, Multibootusb rpms
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

# Enable Cockpit autostart
sudo systemctl enable --now cockpit.socket

# Cleanup
sudo dnf autoremove
sudo dnf clean packages
