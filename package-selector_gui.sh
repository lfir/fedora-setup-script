#!/bin/bash
# This script uses Zenity to create a GUI for selecting packages to install or uninstall
# from a YAML file. It uses Ansible to apply the changes based on the user's selections.

# Check if ansible and zenity are installed and set the playbook file
if ! command -v ansible &> /dev/null; then
    echo 'Error: ansible is not installed.'
    exit 1
fi
if ! command -v zenity &> /dev/null; then
    echo 'Error: zenity is not installed.'
    exit 1
fi
YML_FILE=$(find . -name '*.yml')
if [[ -z "$YML_FILE" ]]; then
    echo 'Error: No .yml file found in the current directory.'
    exit 1
fi

# Line range with Flatpak names to be installed
function getLineNumber() {
    grep -n "$1" "$YML_FILE" | cut -d: -f1
}

R9S=$(( $(getLineNumber 'name: Install Flatpak') + 3 ))
R9E=$(( $(getLineNumber '# GROUPS') - 1 ))
# Range with package names to be removed
R8AUX=$(getLineNumber 'name: Uninstall unused applications')
R8S=$((R8AUX + 3))
R8E=$(( $(getLineNumber 'name: Autoremove unneeded packages') - 2 ))
# Line ranges with package names to be installed
R7AUX=$(getLineNumber 'name: Install Utilities / misc. apps')
R7S=$((R7AUX + 3))
R7E=$((R8AUX - 1))

R6AUX=$(getLineNumber 'name: Install System tools / apps')
R6S=$((R6AUX + 3))
R6E=$((R7AUX - 1))

R5AUX=$(getLineNumber 'name: Install Multimedia applications')
R5S=$((R5AUX + 3))
R5E=$((R6AUX - 1))

R4AUX=$(getLineNumber 'name: Install Internet / networking applications')
R4S=$((R4AUX  + 3))
R4E=$((R5AUX - 1))

R3AUX=$(getLineNumber 'name: Install Hardware drivers & monitoring tools')
R3S=$((R3AUX + 3))
R3E=$((R4AUX - 1))

R2AUX=$(getLineNumber 'name: Install Graphics apps')
R2S=$((R2AUX + 3))
R2E=$((R3AUX - 1))

R1AUX=$(getLineNumber 'name: Install Games / emulation apps')
R1S=$((R1AUX + 3))
R1E=$((R2AUX - 1))

R0S=$(( $(getLineNumber 'name: Install Development tools') + 3 ))
R0E=$((R1AUX - 1))

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
