function configureExternalLinks(root = document) {
  root.querySelectorAll("a[href]").forEach((link) => {
    const url = new URL(link.href, document.baseURI);
    if (url.origin !== window.location.origin) {
      link.target = "_blank";
      link.rel = "noopener noreferrer";
    }
  });
}

configureExternalLinks();

if (typeof document$ !== "undefined") {
  document$.subscribe(() => configureExternalLinks());
}
