#!/bin/bash

# Uninstall unused applications present in the KDE Spin. Add or remove items as needed
sudo dnf remove akregator dnfdragora dragon elisa-player kaddressbook kamoso kde-connect kmag \
  kmahjongg kmail kmines kmousetool kmouth kontact korganizer kpat krdc krfb kwrite mediawriter \
  plasma-discover plasma-systemmonitor

# Enable RPMFusion repositories
sudo sh -c "dnf install https://download1.rpmfusion.org/free/fedora/\
rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm \
https://download1.rpmfusion.org/nonfree/fedora/\
rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm"

# Update base system
sudo dnf --refresh upgrade

# Install package groups
sudo dnf groupinstall Fonts LibreOffice

# Install additional applications. Array names loosely follow submenu names in KDE's app launcher
devel=(docker-compose git java-latest-openjdk-devel java-latest-openjdk-javadoc meld \
  moby-engine nodejs-yarn npm pipenv python-django-bash-completion python3-devel python3-ipython \
  python3-virtualenv ShellCheck)
games=(knights lutris q4wine steam wine winetricks)
hw=(hddtemp kmod-wl lm_sensors lshw radeontop stress)
inet=(chromium filezilla ktorrent remmina wireshark)
multimedia=(asciinema bchunk ffmpeg simplescreenrecorder smplayer)
system=(akmod-VirtualBox beesu cockpit cockpit-machines cockpit-selinux dkms exfat-utils finger \
  gnome-nettool grsync grub-customizer htop iftop iotop kcron kernel-devel ksysguard ksystemlog \
  policycoreutils-gui qemu-kvm sysstat virt-manager VirtualBox VirtualBox-server)
utils=(detox filelight gtkhash hunspell-es kate keepassxc kinfocenter knotes krename kruler \
  moreutils-parallel ncdu nfoview papirus-icon-theme p7zip ranger tldr unrar vim xchm xdotool)

sudo dnf install "${devel[@]}" "${games[@]}" "${hw[@]}" "${inet[@]}" \
  "${multimedia[@]}" "${system[@]}" "${utils[@]}"

# Enable Flathub repository
flatpak remote-add --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo

# Add current user to administration groups
for group in docker libvirt vboxsf vboxusers ; do
  getent group "$group" || sudo groupadd "$group" ; sudo groupmems -a "$USER" -g "$group"
done

# Enable Cockpit autostart
sudo systemctl enable --now cockpit.socket

# Cleanup
sudo dnf autoremove
sudo dnf clean packages

