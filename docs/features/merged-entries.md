# Merged entries

Merged entries group versions of the same title from multiple sources under one library entry. This keeps alternatives close while avoiding duplicate top-level items in the library, updates, and history.

All members must belong to the active profile and use the same entry type. Entries of different types cannot be merged.

## Create a group

Create a group in either of these ways:

- Select compatible entries in the library and use the merge action.
- Open an entry, choose **Merge into library**, and select an existing library entry as the target.

Review the members and confirm. The selected target becomes the **root** used for the group's normal library, updates, and history presentation. A member can still be opened directly, where Katari identifies its root.

The editor order matters: top to bottom is the order Katari uses when composing the group's child list. The root supplies the primary entry presentation, while member sources contribute their available child items.

## Work with a group

Open the root to see the merged child sequence. A member opened directly displays a notice that it belongs to a merged entry and offers actions to open or manage the root.

Use **Manage merged entry** to:

- Reorder members
- Choose a different root
- Remove members from the group
- Optionally remove selected members from the library

If the current root is removed, choose or allow another remaining member to become the root. A group with too few members is dissolved.

## Progress and downloads

Each underlying child retains its source identity. Progress, history, and downloads continue to refer to the actual member and child selected. Merging entries does not copy media between sources or guarantee that differently named child items are equivalent.

## Migration and profiles

Merged groups are profile-specific. Selecting any member for **Move to profile** moves and recreates the complete ordered group in the destination. Destination conflicts can detach or remove entries already present there. Source migrations or extension changes that alter entry URLs can also prevent a stored member from resolving correctly.
