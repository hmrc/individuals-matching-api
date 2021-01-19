<p>Use this endpoint to match an individual's first name, last name, date of birth and National Insurance number (nino) against HMRC’s records. On a successful match, a matchId is returned.</p>
<p>For a successful match, the following set of criteria must be met. </p>
<table>
  <thead>
    <tr>
      <th>Parameter</th>
      <th>Match Criteria</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><p>firstName</p></td>
      <td><p>First character must be identical; all remaining characters are ignored</p></td>
    </tr>    
    <tr>
      <td><p>lastName</p></td>
      <td><p>First three characters must be identical; all remaining characters are ignored</p></td>
    </tr>    
    <tr>
      <td><p>nino</p></td>
      <td><p>All characters must be identical</p></td>
    </tr>
    <tr>
      <td><p>dateOfBirth</p></td>
      <td><p>All characters must be identical</p></td>
    </tr>
  </tbody>
</table>
<p>You will get a subset of the JSON response shown below based on your assigned scopes.</p>