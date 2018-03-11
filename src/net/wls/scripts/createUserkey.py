from weblogic.security.internal import *
from weblogic.security.internal.encryption import *
encryptionService = SerializedSystemIni.getEncryptionService(".")
clearOrEncryptService = ClearOrEncryptedService(encryptionService)
 
# Take encrypt password from user
adminUser   = '@@USER@@'
adminPass   = '@@PASS@@'
adminURL    = '@@ADMINURL@@'
adminConfig = '@@ADMINCONFIG@@'
adminKey    = '@@ADMINKEY@@'
 
connect(username=adminUser, password=adminPass, url=adminURL)
storeUserConfig(userConfigFile=adminConfig, userKeyFile=adminKey, nm='false')

