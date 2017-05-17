from weblogic.security.internal import *
from weblogic.security.internal.encryption import *
encryptionService = SerializedSystemIni.getEncryptionService(".")
clearOrEncryptService = ClearOrEncryptedService(encryptionService)
 
# Take encrypt password from user
user = '@@USER@@'.replace("\\", "")
pass = '@@PASS@@'.replace("\\", "")
nuse = '@@NMUS@@'.replace("\\", "")
npas = '@@NMPA@@'.replace("\\", "")
 
if user.find("{AES}") != -1:
   print "User: " + clearOrEncryptService.decrypt(user)
else
   print "User: "+user


if pass.find("{AES}") != -1:
   print "Pass: " + clearOrEncryptService.decrypt(pass)
else      
   print "Pass: "+pass


if nuse.find("{AES}") != -1:
   print "NMUser: " + clearOrEncryptService.decrypt(nuse)
else      
   print "NMUser: "+nuse

if npas.find("{AES}") != -1:
   print "NMPass:" + clearOrEncryptService.decrypt(npas)
else      
   print "NMPass:"+npas
 
