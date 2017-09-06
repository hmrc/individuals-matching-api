<table>
    <col width="25%">
    <col width="35%">
    <col width="40%">
    <thead>
        <tr>
            <th>Scenario</th>
            <th>Request Payload</th>
            <th>Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><p>Request with a valid payload</p></td>
            <td>
                <p class ="code--block"> {<br>
                                           "nino" : "BB123456B",<br>
                                           "firstName" : "John",<br>
                                           "lastName" : "Smith",<br>
                                           "dateOfBirth" : "1975-05-25"<br>
                                         }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">303 (See Other)</code></p>
                <p class="code--block">Location: /individuals/matching/08aa2149-5e6c-4f3d-8769-4b025db6ba42</p>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid nino, no first name, invalid data type for last name and a date of birth which does not exist</p></td>
            <td>
                <p class ="code--block"> {<br>
                                         	"nino" : "LE241131E",<br>
                                            "lastName" : true,<br>
                                            "dateOfBirth" : "1989-02-30"<br>
                                         }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "BAD_REQUEST",<br>
                                            "message": "Bad Request"<br>
                                            "errors": [<br>
                                            {<br>
                                                  "code": "INVALID_FORMAT",<br>
                                                  "message": "Invalid format has been used",<br>
                                                  "path": "/nino"<br>
                                                },<br>
                                                {<br>
                                                  "code": "INVALID_DATA_TYPE",<br>
                                                  "message": "Invalid data type has been used",<br>
                                                  "path": "/lastName"<br>
                                                },<br>
                                                {<br>
                                                  "code": "INVALID_DATE",<br>
                                                  "message": "Date is invalid",<br>
                                                  "path": "/dateOfBirth"<br>
                                                },<br>
                                                {<br>
                                                  "code": "MISSING_FIELD",<br>
                                                  "message": "This field is required",<br>
                                                  "path": "/firstName"<br>
                                                }<br>
                                            ]<br>
                                         }
                </p>
            </td>
        </tr>
        <tr>
        	 <td><p>Request with a valid payload, but the individual could not be found</p></td>
	        <td>
	            <p class ="code--block"> {<br>
                                                "nino" : "SE235112A",<br>
                                                "firstName" : "Raj",<br>
                                                "lastName" : "Patel",<br>
                                                "dateOfBirth" : "1984-10-30"<br>
                                             }
	            </p>
	        </td>
	        <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
	            <p class ="code--block"> {<br>
                                              "code": "MATCHING_FAILED",<br>
                                              "message": "The individual details provided did not match with HMRC’s records."<br>
                                            }
	            </p>
	        </td>
        </tr>
        <tr>
             <td><p>Request sent to incorrect endpoint</p><p class ="code--block">Endpoint: /individuals/mtch</p></td>
            <td>
                <p class ="code--block"> {<br>
                                                "nino" : "BC234567C",<br>
                                                "firstName" : "Steven",<br>
                                                "lastName" : "Smith",<br>
                                                "dateOfBirth" : "1947-08-15"<br>
                                             }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
                <p class ="code--block"> {<br>
                                              "code": "MATCHING_RESOURCE_NOT_FOUND",<br>
                                              "message": "A resource with the name in the request can not be found in the API"<br>
                                            }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid 'Accept' header</p><p class ="code--block">Accept: application/vnd.hmrc.1.0</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "nino" : "CC123456C",<br>
                                            "firstName" : "Jane",<br>
                                            "lastName" : "Doe",<br>
                                            "dateOfBirth" : "1969-06-09"<br>
                                          }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">406 (Not Acceptable)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "ACCEPT_HEADER_INVALID",<br>
                                            "message": "The accept header is missing or invalid"<br>
                                          }
                </p>
            </td>
        </tr>
	</tbody>
</table>