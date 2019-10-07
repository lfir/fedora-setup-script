git clone https://gitlab.com/asta86/fss.git

sudo cryptsetup luksOpen /dev/sdc1 encrypteddrive
sudo mount /dev/mapper/encrypteddrive /mnt/temp0
sudo umount /mnt/temp0 
sudo cryptsetup luksClose /dev/mapper/encrypteddrive
