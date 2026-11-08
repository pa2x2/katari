package mihon.presentation.kit.theme

/** The font family the desktop sets its interface text in, which the kit's typography uses on desktop. */
interface SystemInterfaceFont {
    /** The family's name as the system's font manager knows it; null where it can't be found. */
    val familyName: String?
}
