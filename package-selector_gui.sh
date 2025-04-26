#!/bin/bash
# This script uses Zenity to create a GUI for selecting packages to install or uninstall
# from a YAML file. It uses Ansible to apply the changes based on the user's selections.

# Check if ansible and zenity are installed
if ! command -v ansible &> /dev/null; then
    echo 'Error: ansible is not installed.'
    exit 1
fi
if ! command -v zenity &> /dev/null; then
    echo 'Error: zenity is not installed.'
    exit 1
fi

YML_FILE='F42_post_install.yml'
# Line ranges with package names to be installed
R0S=34
R0E=47
R1S=$((R0E + 4))
R1E=53
R2S=$((R1E + 4))
R2E=58
R3S=$((R2E + 4))
R3E=72
R4S=$((R3E + 4))
R4E=86
R5S=$((R4E + 4))
R5E=92
R6S=$((R5E + 4))
R6E=111
R7S=$((R6E + 4))
R7E=129
# Line range with package names to be removed
R8S=$((R7E + 4))
R8E=150
# Range with Flatpak names to be installed
R9S=$((R8E + 8))
R9E=159

# Parse package names from specific line ranges in the YAML file
NAME_PATTERN='s/^\s*-\s*//'
PKGS_I=$(sed -n "${R0S},${R0E}p; ${R1S},${R1E}p; ${R2S},${R2E}p; ${R3S},${R3E}p; ${R4S},${R4E}p; ${R5S},${R5E}p; ${R6S},${R6E}p; ${R7S},${R7E}p"\
    "$YML_FILE" | sed -n "${NAME_PATTERN}p")
PKGS_R=$(sed -n "${R8S},${R8E}p" "$YML_FILE" | sed -n "${NAME_PATTERN}p")
PKGS_F=$(sed -n "${R9S},${R9E}p" "$YML_FILE" | sed -n "${NAME_PATTERN}p")

# Create Zenity checklist dialogs
ERRORMSG='Dialog canceled. Exiting.'
SELECTED_PKGSI=$(echo "$PKGS_I" | zenity --list --checklist --title='Select Packages' \
    --text='Select the packages you want to install:' --column='Select' --column='Name' \
    $(echo "$PKGS_I" | sed 's/^/TRUE /') --width=400 --height=900)
if [ $? -ne 0 ]; then
    echo "$ERRORMSG"
    exit 1
fi

SELECTED_PKGSR=$(echo "$PKGS_R" | zenity --list --checklist --title='Select Packages' \
    --text='Select the packages you want to uninstall:' --column='Select' --column='Name' \
    $(echo "$PKGS_R" | sed 's/^/TRUE /') --width=400 --height=900)
if [ $? -ne 0 ]; then
    echo "$ERRORMSG"
    exit 1
fi

SELECTED_PKGSF=$(echo "$PKGS_F" | zenity --list --checklist --title='Select Flatpaks' \
    --text='Select the Flatpaks you want to install:' --column='Select' --column='Name' \
    $(echo "$PKGS_F" | sed 's/^/TRUE /') --width=400 --height=900)
if [ $? -ne 0 ]; then
    echo "$ERRORMSG"
    exit 1
fi

# Convert selected packages to an array
IFS='|' read -r -a IN_ARRAY <<< "$SELECTED_PKGSI"
IFS='|' read -r -a RM_ARRAY <<< "$SELECTED_PKGSR"
IFS='|' read -r -a FP_ARRAY <<< "$SELECTED_PKGSF"
# Remove unselected packages and use a temporary file to store the updated YAML
TMP_FILE=$(mktemp)
CURR_LINE=0
while IFS= read -r line; do
    ((CURR_LINE++))
    # Check if the current line is within one of the defined ranges
    if { [[ $CURR_LINE -ge $R0S && $CURR_LINE -le $R0E ]] || \
         [[ $CURR_LINE -ge $R1S && $CURR_LINE -le $R1E ]] || \
         [[ $CURR_LINE -ge $R2S && $CURR_LINE -le $R2E ]] || \
         [[ $CURR_LINE -ge $R3S && $CURR_LINE -le $R3E ]] || \
         [[ $CURR_LINE -ge $R4S && $CURR_LINE -le $R4E ]] || \
         [[ $CURR_LINE -ge $R5S && $CURR_LINE -le $R5E ]] || \
         [[ $CURR_LINE -ge $R6S && $CURR_LINE -le $R6E ]] || \
         [[ $CURR_LINE -ge $R7S && $CURR_LINE -le $R7E ]]; }; then
        PKG_NAME=$(echo "$line" | sed "${NAME_PATTERN}")
        if [[ ! " ${IN_ARRAY[*]} " =~ " $PKG_NAME " ]]; then
            echo "Delete pkg from playbook: $PKG_NAME"
            continue
        fi
    fi
    if [[ $CURR_LINE -ge $R8S && $CURR_LINE -le $R8E ]]; then
        PKG_NAME=$(echo "$line" | sed "${NAME_PATTERN}")
        if [[ ! " ${RM_ARRAY[*]} " =~ " $PKG_NAME " ]]; then
            echo "Delete pkg from playbook: $PKG_NAME"
            continue
        fi
    fi
    if [[ $CURR_LINE -ge $R9S && $CURR_LINE -le $R9E ]]; then
        PKG_NAME=$(echo "$line" | sed "${NAME_PATTERN}")
        if [[ ! " ${FP_ARRAY[*]} " =~ " $PKG_NAME " ]]; then
            echo "Delete pkg from playbook: $PKG_NAME"
            continue
        fi
    fi
    echo "$line" >> "$TMP_FILE"
done < "$YML_FILE"

# Run the new playbook
ansible-playbook -K "$TMP_FILE"
