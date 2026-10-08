<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Formulaire Employe</title>
</head>
<body>
    <form action="${pageContext.request.contextPath}/employe/save" method="POST">
        <label>Nom : <input type="text" name="nom"></label><br>
        <label>Age : <input type="text" name="age"></label><br>
        <label>Salaire : <input type="text" name="salaire"></label><br>
        <button type="submit">Soumettre</button>
    </form>
</body>
</html>
