<table>
  <col width="25%">
  <col width="35%">
  <col width="40%">
  <thead>
    <tr>
      <th>Scenario</th>
      <th>Parameters</th>
      <th>Response</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><p>Successful match</p></td>
      <td><p>The matchId is valid</p></td>
      <td>
        <p>200 (OK)</p>
        <p>Payload as response example above</p>
      </td>
    </tr>
    <tr>
      <td><p>No match</p></td>
      <td><p>There is no match for the information provided.</p></td>
      <td>
        <p>404 (Not Found)</p>
        <p>{ &quot;code&quot; : &quot;NOT_FOUND&quot;,<br/>&quot;message&quot; : &quot;The resource cannot be found&quot; }</p>
      </td>
    </tr>
    <tr>
      <td><p>Expired matchId</p></td>
      <td><p>The matchId has expired.</p></td>
      <td>
        <p>404 (Not Found)</p>
        <p>{ &quot;code&quot; : &quot;NOT_FOUND&quot;,<br/>&quot;message&quot; : &quot;The resource cannot be found&quot; }</p>
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