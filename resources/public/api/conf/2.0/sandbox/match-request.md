<table>
    <col width="100%">
    <thead>
    <tr>
        <th>Valid payload</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td><p>{&quot;firstName&quot;:&quot;Amanda&quot;,&quot;lastName&quot;:&quot;Joseph&quot;,&quot;nino&quot;:&quot;NA000799C&quot;,&quot;dateOfBirth&quot;:&quot;1960-01-15&quot;}</p></td>
    </tr>
    </tbody>
</table>

<table>
    <col width="25%">
    <col width="35%">
    <col width="40%">
    <thead>
    <tr>
        <th>Scenario</th>
        <th>Payload</th>
        <th>Response</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td><p>Successful match</p>
        <td><p>firstName = &quot;A&quot;<br/>lastName = &quot;Joseph&quot;<br/>nino = &quot;NA000799C&quot;<br/>dateOfBirth = &quot;1960-01-15&quot;</p></td>
        <td><p>200 (OK)</p><p>Payload as response example above</p></td>
    </tr>
    <tr>
        <td><p>No match</p></td>
        <td>
            <p>No data found that meets the criteria for a successful match as described in the match criteria section. </p>
        </td>
        <td><p>404 (Not Found)</p>
        <p>{ &quot;code&quot; : &quot;MATCHING_FAILED&quot;,<br/>&quot;message&quot; : &quot;There is no match for the information provided&quot; }</p></td>
    </tr>
    <tr>
          <td><p>Missing firstName &#47; lastName &#47; nino &#47; dateOfBirth</p></td>
          <td>There is at least one required field missing.</td>
          <td><p>400 (Bad Request)</p>
          <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;&#60;field_name&#62; is required&quot; }</p></td>
    </tr>
    <tr>
        <td><p>Malformed nino</p></td>
        <td><p>Any National Insurance number that does not meet the validation rule.</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;Malformed nino submitted&quot; }</p>
        </td>
    </tr>
    <tr>
        <td><p>Missing CorrelationId</p></td>
        <td><p>The CorrelationId is missing. Check the request headers section for what should be included.</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;CorrelationId is required&quot; }</p>
        </td>
    </tr>
    <tr>
        <td><p>Malformed CorrelationId</p></td>
        <td><p>The CorrelationId is in the incorrect format. Check the request headers section for the correct format.</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;Malformed CorrelationId&quot; }</p>
        </td>
    </tr>
  </tbody>
</table>
