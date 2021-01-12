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
      <td>MatchId found</td>
      <td>The matchId is valid</td>
      <td>
        <p>200 (OK)</p>
        <p>Payload as response example above</p>
      </td>
    </tr>
    <tr>
      <td>Invalid matchId</td>
      <td>The matchId is invalid</td>
      <td>
        <p>404 (Not Found)</p>
        <p>{ &quot;code&quot; : &quot;NOT_FOUND&quot;,<br/>&quot;message&quot; : &quot;The resource cannot be found&quot; }</p>
      </td>
    </tr>
    <tr>
      <td>Expired matchId</td>
      <td>The matchId has expired</td>
      <td>
        <p>404 (Not Found)</p>
        <p>{ &quot;code&quot; : &quot;NOT_FOUND&quot;,<br/>&quot;message&quot; : &quot;The resource cannot be found&quot; }</p>
      </td>
    </tr>
    <tr>
        <td><p>Missing CorrelationId</p></td>
        <td><p>CorrelationId header is missing</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;CorrelationId is required&quot; }</p></td>
        </td>
    </tr>
    <tr>
        <td><p>Malformed CorrelationId</p></td>
        <td><p>CorrelationId header is malformed</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;Malformed CorrelationId&quot; }</p></td>
        </td>
    </tr>
  </tbody>
</table>