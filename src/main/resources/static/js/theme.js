(function () {
    var STORAGE_KEY = "theme";

    function getPreferredTheme() {
        var saved = localStorage.getItem(STORAGE_KEY);
        if (saved === "light" || saved === "dark") {
            return saved;
        }
        return "dark";
    }

    function applyTheme(theme) {
        if (theme === "dark") {
            document.documentElement.setAttribute("data-theme", "dark");
        } else {
            document.documentElement.removeAttribute("data-theme");
        }
        var toggle = document.querySelector(".theme-toggle");
        if (toggle) {
            toggle.textContent = theme === "dark" ? "☀" : "☽";
            toggle.setAttribute("aria-label", theme === "dark" ? "ライトモードに切り替え" : "ダークモードに切り替え");
        }
    }

    applyTheme(getPreferredTheme());

    document.addEventListener("DOMContentLoaded", function () {
        var toggle = document.querySelector(".theme-toggle");
        applyTheme(getPreferredTheme());
        if (!toggle) {
            return;
        }
        toggle.addEventListener("click", function () {
            var current = document.documentElement.getAttribute("data-theme") === "dark" ? "dark" : "light";
            var next = current === "dark" ? "light" : "dark";
            localStorage.setItem(STORAGE_KEY, next);
            applyTheme(next);
        });
    });
})();
