#!/bin/bash

# Enable rpmfusion repositories
sudo sh -c "dnf install https://download1.rpmfusion.org/free/fedora/\
rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm \
https://download1.rpmfusion.org/nonfree/fedora/\
rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm"

# Enable docker ce repository
sudo dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo

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
accesories=(ark gtkhash kcalc keepassxc nfoview pluma pyrenamer shutter thunar)
ed=(pspp)
games=(desmume dolphin-emu pcsx2 pcsxr playonlinux pychess q4wine steam \
       visualboyadvance-m wine winetricks)
graph=(eom kcolorchooser kolourpaint okular xchm)
inet=(firefox hexchat kget remmina transmission-gtk wireshark)
devel=(code git icedtea-web java-latest-openjdk-devel java-latest-openjdk-javadoc meld \
       nodejs-yarn npm pipenv python3-devel python3-ipython python3-virtualenv \
       ShellCheck umbrello)
av=(asciinema bchunk vlc)
system=(beesu bleachbit containerd.io docker-ce docker-ce-cli docker-compose finger \
        gnome-disk-utility gnome-nettool gparted grsync htop ksystemlog \
        tmux VirtualBox VirtualBox-server xfce4-terminal)
hw=(radeontop xorg-x11-drv-amdgpu)
misc=(akmods akmod-VirtualBox caja-actions caja-image-converter \
      caja-open-terminal compton dconf-editor dkms flatpak \
      fuse-encfs grub-customizer hddtemp hunspell-en hunspell-es iftop iotop \
      kernel-devel libreoffice-core lightdm-settings libvirt-bash-completion lm_sensors \
      lshw mate-icon-theme-faenza moreutils moreutils-parallel ncdu \
      nitroshare p7zip p7zip-plugins pavucontrol policycoreutils-gui preload \
      procmail qemu-kvm ranger SDL2-devel \
      seahorse-caja smartmontools stress sysstat telnet tldr unrar wget \
      whois xfce4-clipman-plugin yum-utils)

sudo dnf install "${accesories[@]}" "${ed[@]}" "${games[@]}" "${graph[@]}" "${inet[@]}" \
                 "${devel[@]}" "${av[@]}" "${system[@]}" "${hw[@]}" "${misc[@]}"

# Enable flathub repository
flatpak remote-add --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo

# Create applications dir and switch to it
mkdir "$HOME/Applications" && cd "$_" || return

# Install chrome, insync, multibootusb rpms
wget https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm
wget https://d2t3ff60b2tol4.cloudfront.net/builds/insync-3.0.19.40421-fc29.x86_64.rpm
latestM="https://github.com$(wget -O - \
https://github.com/mbusb/multibootusb/releases/latest | grep -Po '/.+[0-9]\.noarch\.rpm')"
wget -O mbusb.rpm "$latestM"

sudo dnf install ./*.rpm

# Remove downloaded rpms
rm -f ./*.rpm

# Add current user to vbox group
sudo groupmems -a "$USER" -g docker -g libvirt -g vboxsf -g vboxusers

# Cleanup
sudo dnf autoremove
sudo dnf clean packages
