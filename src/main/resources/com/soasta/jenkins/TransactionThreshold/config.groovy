package com.soasta.jenkins.TransactionThreshold;

def f = namespace(lib.FormTagLib)
    
f.entry(title:"Transaction Name",field:"transactionname") {
  f.textbox()
}
f.entry(title:"Threshold",field:"thresholdname" ) {
  f.select(name:"thresholdname")
}

f.entry(title:"Threshold Min Value",field:"thresholdminvalue") {
    f.textbox()
  }

f.entry(title:"Threshold Max Value",field:"thresholdmaxvalue") {
    f.textbox()
  }

f.entry {
  div(align:"left") {
    input(type:"button",value:"Delete",class:"repeatable-delete")
  }
}

