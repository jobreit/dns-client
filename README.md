# dns-client

McGill Course work Project. 

Building a DNS client - Requirements:

"Write a program using Java sockets that:<br>
• Is invoked from the command line (STDIN); <br>
• Send a query to the server for the given domain name using a UDP socket;<br>
• Wait for the response to be returned from the server;<br>
• Interpret the response and output the result to terminal display (STDOUT).<br><br>
Your DNS client should support the following features:<br>
• Send queries for A (IP addresses), MX (mail server), and NS (name server) records;<br> 
• Interpret responses that contain A records (IP addresses) and CNAME records (DNS aliases);<br>
• Retransmit queries that are lost;<br><br>
Your client must also handle errors gracefully. In particular, if the response message does not conform to the DNS specification, if it contains fields or entries that cannot be interpreted, or if the client receives a response that does not match the query it sent, then an appropriate error message should be printed to the screen indicating what was unexpected or what went wrong."
