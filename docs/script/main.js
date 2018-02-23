function setTooltip(btn, message) {
    $(btn).tooltip('hide').attr('data-original-title', message).tooltip('show');
}

function hideTooltip(btn) {
    setTimeout(function () {
        $(btn).tooltip('hide').attr('data-original-title', 'Copy to clipboard');
    }, 1000);
}

function wildFlyCard(version) {
    var header = "WildFly " + version;
    var boltId = 'bolt-' + version;
    var boltUrl = 'bolt://model-graph-' + version + '-bolt-model-graph.6923.rh-us-east-1.openshiftapps.com:80';
    var browserUrl = 'http://model-graph-' + version + '-browser-model-graph.6923.rh-us-east-1.openshiftapps.com/browser/';
    var browserTarget = 'neo4j-browser-' + version;

    var cardHtml = '<div class="card"><div class="card-body"><h5 class="card-title">' + header + '</h5>' +
        '<figure class="snippet p-3 bg-light">' +
        '<button class="btn clipboard" data-clipboard-target="#' + boltId + '" data-toggle="tooltip" data-placement="top" title="Copy to clipboard"><img width="13" src="clippy.svg" alt=""></button>' +
        '<code id="' + boltId + '">' + boltUrl + '</code>' +
        '</figure>' +
        '<a target="' + browserTarget + '" href="' + browserUrl + '" class="card-link">Neo4J Browser</a>' +
        '</div></div>';

    var root = document.getElementById("wildfly-cards");
    var column = document.createElement("div");
    column.classList.add("col");
    column.innerHTML = cardHtml;
    root.appendChild(column);
}