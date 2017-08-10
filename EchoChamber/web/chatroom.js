                       
            var webSocket;
            var messages = document.getElementById("messages");
            var name = "";    

            window.onload  = function(){
                openSocket(); 
                var url = window.location.pathname;               
                if( !url.includes("login") && !name ){
                    console.log("isnull");
                }
            }
           
           
           
            function openSocket(){
                // Ensures only one connection is open at a time
                if(webSocket !== undefined && webSocket.readyState !== WebSocket.CLOSED){
                   writeResponse("WebSocket is already opened.");
                    return;
                }
                // Create a new instance of the websocket
                webSocket = new WebSocket("ws://localhost:8080/EchoChamber/echo");
                 
                /**
                 * Binds functions to the listeners for the websocket.
                 */
                webSocket.onopen = function(event){
                    // For reasons I can't determine, onopen gets called twice
                    // and the first time event.data is undefined.
                    // Leave a comment if you know the answer.
                    if(event.data === undefined)
                        return;
 
                    writeResponse(event.data);
                };
 
                webSocket.onmessage = function(event){
                    writeResponse(event.data);
                };
 
                webSocket.onclose = function(event){
                    writeResponse("Connection closed");
                };
            }
           
            /**
             * Sends the value of the text input to the server
             */
            function send(){
                var text = $("#messageinput")[0].value;
                webSocket.send(text);
                writeResponse(text);
                $("#messageinput")[0].value = "";
            }
            
            $(function () {
                $( "body" ).keypress(function(e) {
                     if(e.which === 13) {  
                         send();
                     }
                 });
            });
            
            function closeSocket(){
                webSocket.close();
            }
 
            function writeResponse(text){
                 $("#conversation").append("<tr><td>"+text+"</td></tr>");
            }


            function enterName(){
                var loginBox = $("#loginBox")[0];
                var text = loginBox.value;
                console.log("name");
                webSocket.send("name: " + text);
            }
            
            
                    
