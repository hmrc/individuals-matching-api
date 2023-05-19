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
        <td><p>Happy path</p>
        <p>(Match first letter of first name)</p></td>
        <td><p>firstName = &quot;A&quot;<br/>lastName = &quot;Joseph&quot;<br/>nino = &quot;NA000799C&quot;<br/>dateOfBirth = &quot;1960-01-15&quot;</p></td>
        <td><p>200 (OK)</p><p>Payload as response example above</p></td>
    </tr>
    <tr>
        <td><p>No match</p></td>
        <td>Any nino and dateOfBirth that are not an exact match and firstName does not match on first letter and lastName does not match on the first three letters</td>
        <td><p>403 (Forbidden)</p>
        <p>{ &quot;code&quot; : &quot;MATCHING_FAILED&quot;,<br/>&quot;message&quot; : &quot;There is no match for the information provided&quot; }</p></td>
    </tr>
    <tr>
          <td><p>Missing firstName &#47; lastName &#47; nino &#47; dateOfBirth</p></td>
          <td>Any field missing</td>
          <td><p>400 (Bad Request)</p>
          <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;&#60;field_name&#62; is required&quot; }</p></td>
    </tr>
    <tr>
        <td><p>Malformed nino</p></td>
        <td><p>Any NINO that does not meet the validation rule</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;Malformed nino submitted&quot; }</p></td>
        </td>
    </tr>
  </tbody>
</table>
