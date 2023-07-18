from weblogic.security.internal import *
from weblogic.security.internal.encryption import *
encryptionService = SerializedSystemIni.getEncryptionService(".")
clearOrEncryptService = ClearOrEncryptedService(encryptionService)
 
# Take encrypt password from user
user = '@@USER@@'
past = '@@PASS@@'
nuse = '@@NMUS@@'
npas = '@@NMPA@@'
 
if user.find("{AES}") != -1:
   print "User:" + clearOrEncryptService.decrypt(user)
else:
   print "User:"+user


if past.find("{AES}") != -1:
   print "Pass:" + clearOrEncryptService.decrypt(past)
else:
   print "Pass:"+past


if nuse.find("{AES}") != -1:
   print "NMUser:" + clearOrEncryptService.decrypt(nuse)
else:     
   print "NMUser:"+nuse

if npas.find("{AES}") != -1:
   print "NMPass:" + clearOrEncryptService.decrypt(npas)
else:
   print "NMPass:"+npas
 
