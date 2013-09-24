package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.rule.Driver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginConfigTotpPage {

    private static String PATH = Constants.AUTH_SERVER_ROOT + "/rest/realms/demo/account/totp";

    @Driver
    private WebDriver browser;

    @FindBy(id = "totpSecret")
    private WebElement totpSecret;

    @FindBy(id = "totp")
    private WebElement totpInput;

    @FindBy(css = "input[type=\"submit\"]")
    private WebElement submitButton;

    public void configure(String totp) {
        totpInput.sendKeys(totp);
        submitButton.click();
    }

    public String getTotpSecret() {
        return totpSecret.getAttribute("value");
    }

    public boolean isCurrent() {
        return browser.getTitle().equals("Config TOTP");
    }

    public void open() {
        browser.navigate().to(PATH);
    }

}