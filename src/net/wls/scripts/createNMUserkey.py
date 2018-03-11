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
nmSecure    = 'plain'
nmPort      = '5556'
domainName  = '@@DOMAINNAME@@'
 
nmConnect(username=adminUser, password=adminPass, domainName=domainName, port=nmPort, nmType=nmSecure)
storeUserConfig(userConfigFile=adminConfig, userKeyFile=adminKey, nm='true')

