package com.soasta.jenkins.Composition;

def f = namespace(lib.FormTagLib)
    
   


f.invisibleEntry {
    f.textbox(field:"id");
}
f.entry(title:"Composition Name",field:"name") {
  f.textbox()
}
f.entry(title:"URL",field:"url") {
    f.textbox()
}

f.entry(title:"Username",field:"username") {
    f.textbox()
}

f.entry(title:"Password",field:"password") {
    f.password()
}

f.validateButton(method:"validate",with:"url,username,password,id,name",title:"Test Connection")

f.entry {
  div(align:"left") {
    input(type:"button",value:"Delete",class:"repeatable-delete")
  }
}

