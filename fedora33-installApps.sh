#!/bin/bash

# Remove unused applications present in the KDE Spin
sudo dnf remove calligra-core dnfdragora dragon falkon juk k3b kaddressbook kamoso kde-connect \
  kmag kmahjongg kmail kmines kmousetool kmouth kontact korganizer kpat krdc krfb kruler \
  krusader kwrite mediawriter plasma-discover

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

# Install package groups
sudo dnf groupinstall Fonts LibreOffice

# Install additional applications
devel=(code git java-latest-openjdk-devel java-latest-openjdk-javadoc meld nodejs-yarn npm pipenv \
  python-django-bash-completion python3-devel python3-ipython python3-virtualenv ShellCheck)
educ=(pspp)
games=(desmume knights pcsx2 pcsxr q4wine steam visualboyadvance-m wine winetricks)
hw=(hddtemp lm_sensors radeontop stress xorg-x11-drv-amdgpu)
inet=(filezilla insync remmina wireshark)
misc=(akmod-VirtualBox dkms dnf-utils exfat-utils fuse-encfs hunspell-en \
  hunspell-es kernel-devel mate-themes \
  moreutils-parallel papirus-icon-theme \
  qemu-kvm smartmontools sysstat tldr unrar xdotool)
multimedia=(asciinema bchunk ffmpeg simplescreenrecorder smplayer smtube)
system=(beesu cockpit cockpit-machines cockpit-selinux \
  docker-compose finger gnome-nettool grsync grub-customizer htop iftop iotop \
  ksystemlog lshw moby-engine ncdu policycoreutils-gui virt-manager VirtualBox VirtualBox-server)
utils=(detox filelight gtkhash kate kcron keepassxc knotes krename nfoview \
  p7zip p7zip-plugins ranger vim xchm)

sudo dnf install "${devel[@]}" "${educ[@]}" "${games[@]}" "${hw[@]}" \
  "${inet[@]}" "${misc[@]}" "${multimedia[@]}" "${system[@]}" "${utils[@]}"

# Enable Flathub repository
flatpak remote-add --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo

# Create applications' directory and switch to it
mkdir "$HOME/Applications" && cd "$_" || return

# Install Chrome rpm
wget https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm

sudo dnf install ./*.rpm

# Remove downloaded rpms
rm -f ./*.rpm

# Add current user to administration groups
sudo groupmems -a "$USER" -g docker -g libvirt -g vboxsf -g vboxusers

# Enable Cockpit autostart
sudo systemctl enable --now cockpit.socket

# Cleanup
sudo dnf autoremove
sudo dnf clean packages
