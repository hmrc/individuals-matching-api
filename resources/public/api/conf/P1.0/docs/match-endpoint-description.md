<p>This resource is used to match an individual's first name, last name, date of birth and National Insurance Number (NINO) against HMRC’s records. On a successful match, a matchId is returned.</p>
<p>The following set of criteria must all be met for a successful match of the data provided, against HMRC’s records:</p>
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
