// Copy buttons for one-time codes and passwords. The pages work without it: the code is shown as text either way.
document.querySelectorAll("[data-copy]").forEach((button) => {
  const target = document.getElementById(button.dataset.copy);
  if (!target) return;
  button.hidden = false;
  button.addEventListener("click", async () => {
    const text = target.textContent.trim();
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      // The clipboard API needs HTTPS or localhost; selecting the text lets the person copy it themselves.
      const range = document.createRange();
      range.selectNodeContents(target);
      const selection = window.getSelection();
      selection.removeAllRanges();
      selection.addRange(range);
      return;
    }
    button.textContent = "Copied";
    setTimeout(() => { button.textContent = "Copy"; }, 2000);
  });
});
