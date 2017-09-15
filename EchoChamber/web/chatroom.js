                       
            var webSocket;
            var messages = document.getElementById("messages");
            var name = "Anonymous";  
            var userID = -1;


            console.log(Date.now());

            window.onload  = function(){
                openSocket(); 
                var url = window.location.pathname;               
                if( !url.includes("login") && !name ){
                    console.log("isnull");
                }
                if(name !== ""){
                     $("#title")[0].replaceWith(name);
                }


            }
           
          
           
           
            function openSocket(){
                 console.log("open");
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
                    
                    if(event.data === undefined){
                        return;
                    }
                    
                    writeResponse(event.data);
                   
                };
 
                webSocket.onmessage = function(event){
                    parseXMLText(event.data);
                };
 
                webSocket.onclose = function(event){
                    writeResponse("Connection closed");
                };
            }
           
            /**
             * Sends the value of the text input to the server
             */
            function send_chat_data(){
                var text = $("#messageinput")[0].value;
                
                if(text.length > 0){
                    var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

                    xml += "<body>";
                    xml += addContentToXML("content","chat_data" );
                    xml += addContentToXML("UserID",userID );
                    xml += addContentToXML("text", text );
                    xml += addContentToXML("timeStamp", Date.now()  ); 
                    xml += addContentToXML("name", name  ); 
                    xml += "</body>";

                    webSocket.send(xml);
                    $("#messageinput")[0].value = "";
                }
                
            }
            
            $(function () {
                $( "body" ).keypress(function(e) {
                     if(e.which === 13) {  
                         send_chat_data();
                     }
                     
                 });
            });
            
            function closeSocket(){
                webSocket.close();
            }
 
            function writeResponse( text){

                 $("#conversation").append("<tr><td>"+text+"</td></tr>");
            }
                       
            function updateChat(user, text, timestamp){
                 $("#conversation").append("<tr><td>"+user+"</td><td>"+text+"</td><td>"+timestamp+"</td></tr>");
            } 


            function login(){
                var loginBox = $("#loginBox")[0];
                var login = loginBox.value;
                var passwordBox = $("#passwordBox")[0];
                var password = passwordBox.value;
                
                var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

                xml += "<body>";
                xml += addContentToXML("content","login_data" );
                xml += addContentToXML("login",login );
                xml += addContentToXML("password", password );
                xml += addContentToXML("timestamp", Date.now()  );               
                xml += "</body>";

                webSocket.send(xml);
                $("#messageinput")[0].value = "";
                
                
                
            }
            
            function newUser(){
                var newUserBox = $("#NewName")[0];
                var newUser = newUserBox.value;
                var passwordBox = $("#NewPassword")[0];
                var password = passwordBox.value;
                
                var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

                xml += "<body>";
                xml += addContentToXML("content","new_user" );
                xml += addContentToXML("new_user",newUser );
                xml += addContentToXML("password", password );
                xml += addContentToXML("timestamp", Date.now()  );               
                xml += "</body>";
                webSocket.send(xml);
                
            }
            
            
            function addContentToXML(tag, content){
                 return '<'+ tag + '>' + content + '</' + tag + '>';
            }
            
            function parseXMLText(xml){
                // parse XML and use it to make messages
                if(xml.includes("xml")){
                    parser = new DOMParser();
                    xmlDoc = parser.parseFromString(xml,"text/xml");
                    
                    var content = xmlDoc.getElementsByTagName("content")[0].childNodes[0].nodeValue;
                    if(content ==="login" || content === "new_user"){
                        var user_id = xmlDoc.getElementsByTagName("user_id")[0].childNodes[0].nodeValue;
                        var name = xmlDoc.getElementsByTagName("name")[0].childNodes[0].nodeValue;
                        loginAs(name,user_id);
                    }
                    else{

                        var User_name = xmlDoc.getElementsByTagName("name")[0].childNodes[0].nodeValue;
                        var text = xmlDoc.getElementsByTagName("text")[0].childNodes[0].nodeValue;
                        var timestamp = xmlDoc.getElementsByTagName("timeStamp")[0].childNodes[0].nodeValue;
                        var ts = parseInt(timestamp);
                        var date = new Date(ts);
                        var monthNames = ["January", "February", "March", "April", "May", "June",
          "July", "August", "September", "October", "November", "December"
        ];
                        var hours = date.getHours()%12;
                        hours = hours == 0 ? 12:hours; 
                        var ampm = date.getHours() >= 12 ? 'pm' : 'am';



                        var dateString =  monthNames[date.getMonth()] + " " + date.getDate() + ", " + date.getFullYear()+ " at " + hours + ":" + date.getMinutes() + " " + ampm;
                        updateChat(User_name,text,dateString);
                    }
                //console.log("xml:" + u + " "  + t + " " + ts);
                }
                
            }
            
            function loginAs( loginName,  loginId){
                if(loginId != -1){
                            if($("#NewName").length > 0){
                               var newUserBox = $("#NewName")[0]; 
                               newUserBox.value = "";
                            }
                             if($("#NewPassword").length > 0){
                                var passwordBox = $("#NewPassword")[0]; 
                               passwordBox.value = "";
                            }
                            
                            userID = loginId;
                            name = loginName;                            
                            $("#signin").hide();
                            $("#signup").hide();
                            
                        //    console.log(name);
                         //   alert("user: " + user);
                        //    document.location.href = "http://localhost:8080/EchoChamber/";
                           
                           
                        }
                        else{
                            alert("name or password does not exist")
                        }
            }
                    

