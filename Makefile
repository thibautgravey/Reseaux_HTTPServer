default :
	@echo all, compile, launch, setPermission, unsetPermission

all : compile launch

compile :
	javac -d bin/ src/http/client/WebPing.java src/http/server/WebServer.java src/http/server/StatusCode.java

launch : 
	java -cp bin/ http.server.WebServer 3000

setPermission :
	chmod ugo+rwx res/text.txt

unsetPermission :
	chmod ugo-rwx res/text.txt