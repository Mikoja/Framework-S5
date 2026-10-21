<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Upload de fichier</title>
</head>
<body>
    <form action="${pageContext.request.contextPath}/upload/save" method="POST" enctype="multipart/form-data">
        <label>Fichier : <input type="file" name="fichier"></label><br>
        <label>Description : <input type="text" name="description"></label><br>
        <button type="submit">Envoyer</button>
    </form>
</body>
</html>
