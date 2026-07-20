<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${titre}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        table { border-collapse: collapse; width: 100%; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
        tr:nth-child(even) { background-color: #f2f2f2; }
        .entree { color: green; font-weight: bold; }
        .sortie { color: red; font-weight: bold; }
    </style>
</head>
<body>
    <h1>${titre}</h1>

    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Type</th>
                <th>Description</th>
                <th>Montant</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="m" items="${mouvements}">
                <tr>
                    <td>${m.id}</td>
                    <td class="${m.type == 'ENTREE' ? 'entree' : 'sortie'}">${m.type}</td>
                    <td>${m.description}</td>
                    <td>${m.montant} Ar</td>
                </tr>
            </c:forEach>
        </tbody>
    </table>

    <p><a href="${pageContext.request.contextPath}/">Accueil</a></p>
</body>
</html>
