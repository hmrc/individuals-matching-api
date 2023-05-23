<table>
    <col width="25%">
    <col width="35%">
    <col width="40%">
    <thead>
        <tr>
            <th>Scenario</th>
            <th>Request Payload</th>
            <th>Example Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><p>Request with a valid UUID</p></td>
            <td>
                <p>N/A</p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (Ok)</code></p>
                <p class="code--block">
                    {<br>
                    "_links": [<br>
                    {<br>
                    "name": "self",<br>
                    "href": "/individuals/matching/633e0ee7-315b-49e6-baed-d79c3dffe467"<br>
                    },<br>
                    {<br>
                    "name": "ras",<br>
                    "href": "/individuals/matched/633e0ee7-315b-49e6-baed-d79c3dffe467/get-residency-status"<br>
                    }<br>
                    ]<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid UUID</p></td>
            <td>
                <p>N/A</p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVALID_UUID",<br>
                                            "message": "The match has timed out and the UUID is no longer valid.
                                                        The match (POST to /individuals/matching) will need to be repeated."<br>
                                         }<br>
                </p>
            </td>
        </tr>
        <tr>
             <td><p>Request sent to incorrect endpoint</p><p class ="code--block">Endpoint: /individuals/mtch/{UUID}</p></td>
            <td>
                <p>N/A</p>
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
            <td><p>Request with a valid UUID and an invalid 'Accept' header</p><p class ="code--block">Accept: application/vnd.hmrc.1.0</p></td>
            <td>
                <p>N/A</p>
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
