declare namespace App {
  interface Locals {
    /** The sections of the guide page being rendered, recorded by each Task and Question as it renders; see GuidePage. */
    guideSections?: import("./guide/sections.ts").GuideSection[];
  }
}
