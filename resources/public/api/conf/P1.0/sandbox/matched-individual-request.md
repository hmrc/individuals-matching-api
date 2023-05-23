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
      <td>Happy path</td>
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
  </tbody>
</table>
