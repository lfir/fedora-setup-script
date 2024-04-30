#!/bin/bash

# Uninstall unused applications present in the KDE Spin. Add or remove items as needed
sudo dnf remove akregator dragon elisa-player kaddressbook kde-connect kmahjongg kmail kmines \
  kmouth korganizer kpat krdc krfb ktnef kwrite neochat plasma-welcome skanpage spectacle

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
  moby-engine npm python3-devel python3-ipython python3-virtualenv ShellCheck yarnpkg)
games=(lutris q4wine steam wine winetricks)
hw=(hddtemp kmod-wl lm_sensors lshw nvtop stress)
inet=(chromium filezilla ktorrent remmina wireshark)
multimedia=(asciinema bchunk simplescreenrecorder smplayer)
system=(akmod-VirtualBox beesu cockpit cockpit-machines cockpit-selinux dkms exfat-utils finger \
  gnome-nettool grsync grub-customizer htop iftop iotop kcron kernel-devel ksystemlog \
  policycoreutils-gui qemu-kvm sysstat virt-manager VirtualBox VirtualBox-server)
utils=(clamav clamav-update detox flameshot gtkhash hunspell-es kate keepassxc \
  krename kruler moreutils-parallel ncdu nfoview papirus-icon-theme ranger tldr unrar vim \
  xchm xdotool)

sudo dnf install "${devel[@]}" "${games[@]}" "${hw[@]}" "${inet[@]}" \
  "${multimedia[@]}" "${system[@]}" "${utils[@]}"

# Enable Flathub repository
flatpak remote-add --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo

# Add current user to administration groups
for group in docker libvirt vboxusers ; do
  getent group "$group" || sudo groupadd "$group" ; sudo groupmems -a "$USER" -g "$group"
done

# Enable Cockpit autostart
sudo systemctl enable --now cockpit.socket

# Cleanup
sudo dnf autoremove
sudo dnf clean packages

