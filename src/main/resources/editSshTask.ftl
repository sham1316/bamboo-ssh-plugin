[@ww.textfield labelKey="Host" name="host" required='true'/]
[@ww.textfield labelKey="Username" name="username" required='true'/]

[#if mode == "create"]
    [@ww.password labelKey="Password" name="password" required='true'/]
[#elseif mode == "edit"]
    [@ww.checkbox labelKey="Change Password?" toggle='true' name='change_password'/]
    [@ui.bambooSection dependsOn='change_password' ]
    [@ww.password labelKey="New Password" name="new_password" required='true'/]
    [/@ui.bambooSection]
[/#if]

[@ww.textarea labelKey="Inline Script" name="inlineScript" required='true'/]
[@ww.textfield labelKey="Timeout" name="timeout" required='true'/]