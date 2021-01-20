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
      <td><p>MatchId found</p></td>
      <td><p>The matchId is valid</p></td>
      <td>
        <p>200 (OK)</p>
        <p>Payload as response example above</p>
      </td>
    </tr>
    <tr>
      <td><p>Invalid matchId</p></td>
      <td><p>The matchId is not valid</p></td>
      <td>
        <p>404 (Not Found)</p>
        <p>{ &quot;code&quot; : &quot;NOT_FOUND&quot;,<br/>&quot;message&quot; : &quot;The resource cannot be found&quot; }</p>
      </td>
    </tr>
    <tr>
      <td><p>Expired matchId</p></td>
      <td><p>The matchId has expired</p></td>
      <td>
        <p>404 (Not Found)</p>
        <p>{ &quot;code&quot; : &quot;NOT_FOUND&quot;,<br/>&quot;message&quot; : &quot;The resource cannot be found&quot; }</p>
      </td>
    </tr>
  </tbody>
</table>